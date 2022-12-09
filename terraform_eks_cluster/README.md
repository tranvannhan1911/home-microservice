# Terraform - Create EKS cluster

![Untitled](https://i.imgur.com/Q4p15Ah.png)

1. Mục tiêu
    - Sử dụng terraform để xây dựng hạ tầng như sơ đồ trên.
    - Tạo một EKS Cluster, với các node nằm trong private subnet
    - Đảm bảo high availability
    - Thực hiện deploy application lên EKS Cluster
        - Tạo ingress controller, khi người dùng request ingress controller sẽ route traffic đến service.
        - Từ service sẽ route các traffic tới các pod khả dụng.
2. Nội dung
    1. Cài đặt và cấu hình `awscli`
    2. Cài đặt `kubectl`
    3. Tạo project terraform
        - Cấu trúc file
            
            ```yaml
            |   iam.tf
            |   main.tf
            |   node_group_keypair.pub
            |   provider.tf
            |   variables.tf
            \---vpc
                |   data_source.tf
                |   igw.tf
                |   nat_gw.tf
                |   provider.tf
                |   route_table.tf
                |   subnet.tf
                |   variables.tf
                |   vpc.tf
            ```
            
        1. Tạo module VPC
            - Tóm tắt
                - `VPC` bao gồm 1 `public subnet` và 1 `private subnet` ở mỗi `AZ`
                - Với mỗi `AZ` đặt một `NAT gateway` để có thể kết nối internet, các service bên ngoài từ các instance trong `private subnet`. Mỗi `NAT gateway` sẽ được allocate với một `EIP` (Elastic IP)
                - Tạo 3 `route table` cho 3 `private subnet`, thêm rule đi từ `private subnet` ra internet thông qua `NAT gateway` của `AZ` tương ứng.
                - Tạo 1 `route table` cho 3 `public subnet`, thêm rule đi từ `public subnet` ra internet thông qua `IGW`.
                - Output `vpc`, `public subnets`, `private subnets`
            - Tạo module `vpc` (Tạo thư mục `vpc`)
            - `variables.tf`
                
                ```bash
                variable "region" {
                  default = "ap-southeast-1"
                }
                variable "cidr_block" {
                  default = "10.0.0.0/16"
                }
                variable "cidr_block_public_subnet" {
                  default = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
                }
                variable "cidr_block_private_subnet" {
                  default = ["10.0.4.0/24", "10.0.5.0/24", "10.0.6.0/24"]
                }
                ```
                
                - Chứa các biến được sử dụng trong module
                - `region`: region aws
                - `cidr_block`: cidr block được sử dụng cho VPC
                - `cidr_block_public_subnet`: cidr block được sử dụng cho public subnet, các cidr block này phải là tập con của cidr block của VPC, và không được conflict với nhau (không chồng lẫn lên nhau).
                - `cidr_block_private_subnet`: cidr block được sử dụng cho private subnet, các cidr block này phải là tập con của cidr block của VPC, và không được conflict với nhau (không chồng lẫn lên nhau).
            - Tạo `vpc` (vpc.tf)
                - [https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/vpc](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/vpc)
                
                ```bash
                resource "aws_vpc" "eks_vpc" {
                  cidr_block = var.cidr_block
                
                  tags = {
                    Name = "EKS VPC"
                  }
                }
                ```
                
                - `cidr_block`: là cidr block đã được khai báo trong `variables.tf`
            - Lấy danh sách `availability zone` trong `region` (data_source.tf)
                
                ```bash
                data "aws_availability_zones" "az" {
                  state = "available"
                }
                ```
                
                - `state = "available"`: Chỉ trả về các AZ khả dụng
            - Tạo public subnet (subnet.tf)
                - [https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/subnet](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/subnet)
                
                ```bash
                resource "aws_subnet" "public_subnet" {
                  count                   = length(var.cidr_block_public_subnet)
                  vpc_id                  = aws_vpc.eks_vpc.id
                  cidr_block              = var.cidr_block_public_subnet[count.index]
                  availability_zone       = data.aws_availability_zones.az.names[count.index]
                  map_public_ip_on_launch = true
                
                  tags = {
                    Name = "EKS public subnet ${count.index + 1}"
                  }
                }
                ```
                
                - `count = length(var.cidr_block_public_subnet)`: Với mỗi `AZ` tạo một `public subnet` tương ứng.
                - `vpc_id = aws_vpc.eks_vpc.id`: `VPC` đã tạo phía trên
                - `cidr_block = var.cidr_block_public_subnet[count.index]`: Lấy `cidr block` trong danh sách `cidr_block_public_subnet` ở vị trí `count.index`
                - `availability_zone = data.aws_availability_zones.az.names[count.index]`: Lấy `AZ` từ danh sách `AZ` ở vị trí `count.index`
                - `map_public_ip_on_launch = true`: Nếu các instance được tạo trong subnet này, nó sẽ được gán địa chỉ IP public.
            - Tạo private subnet (subnet.tf)
                - [https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/subnet](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/subnet)
                
                ```bash
                resource "aws_subnet" "private_subnet" {
                  count             = length(var.cidr_block_private_subnet)
                  vpc_id            = aws_vpc.eks_vpc.id
                  cidr_block        = var.cidr_block_private_subnet[count.index]
                  availability_zone = data.aws_availability_zones.az.names[count.index]
                
                  tags = {
                    Name = "EKS private subnet ${count.index + 1}"
                  }
                }
                ```
                
                - Tương tự như public subnet
            - Tạo igw (igw.tf)
                - [https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/internet_gateway](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/internet_gateway)
                
                ```bash
                resource "aws_internet_gateway" "igw" {
                  vpc_id = aws_vpc.eks_vpc.id
                
                  tags = {
                    Name = "Internet gateway"
                  }
                }
                ```
                
            - Tạo route table cho public subnet (route_table.tf)
                - [https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/route_table](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/route_table)
                
                ```bash
                resource "aws_route_table" "route_table_public_subnet" {
                  vpc_id = aws_vpc.eks_vpc.id
                  tags = {
                    Name = "Route table public subnet"
                  }
                }
                ```
                
                - Tạo route table `route_table_public_subnet`
                - Tạo route đi từ public subnet ra internet thông qua `igw`
                    - [https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/route](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/route)
                    
                    ```bash
                    resource "aws_route" "route_public_subnet" {
                      route_table_id         = aws_route_table.route_table_public_subnet.id
                      destination_cidr_block = "0.0.0.0/0"
                      gateway_id             = aws_internet_gateway.igw.id
                    }
                    ```
                    
                    - `destination_cidr_block = "0.0.0.0/0"`: Đích đến của route, `0.0.0.0/0` là internet.
                    - `gateway_id = aws_internet_gateway.igw.id`: đi ra internet thông qua `igw`
                - Associate các public subnet vào route table
                    - [https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/route_table_association](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/route_table_association)
                    
                    ```bash
                    resource "aws_route_table_association" "route_table_public_subnet_association" {
                      count          = length(aws_subnet.public_subnet)
                      subnet_id      = aws_subnet.public_subnet[count.index].id
                      route_table_id = aws_route_table.route_table_public_subnet.id
                    }
                    ```
                    
                    - Associate tất cả public subnet vào cùng một route table
            - Tạo `NAT gateway`
                - [https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/eip](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/eip)
                - [https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/nat_gateway](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/nat_gateway)
                
                ```bash
                resource "aws_eip" "eip_nat_gw" {
                  count = length(var.cidr_block_private_subnet)
                }
                
                resource "aws_nat_gateway" "nat_gw" {
                  count         = length(aws_subnet.public_subnet)
                  subnet_id     = aws_subnet.public_subnet[count.index].id
                  allocation_id = aws_eip.eip_nat_gw[count.index].id
                
                  tags = {
                    Name = "NAT Gateway"
                  }
                }
                ```
                
                - Mỗi `NAT gateway` sẽ được gán một địa chỉ ip riêng (EIP)
                - `subnet_id = aws_subnet.public_subnet[count.index].id`: Vị trí mà `nat gateway` được tạo, `nat gateway` sẽ được tạo trong các public subnet, rồi từ public subnet yêu cầu đi ra internet bằng `igw`: `instance` → `nat gateway` → `igw`
            - Tạo route table cho private subnet (route_table.tf)
                
                ```bash
                resource "aws_route_table" "route_table_private_subnet" {
                  count  = length(aws_subnet.private_subnet)
                  vpc_id = aws_vpc.eks_vpc.id
                  tags = {
                    Name = "Route table private subnet"
                  }
                }
                ```
                
                - Với mỗi private subnet tạo các route table tương ứng, vì mỗi private subnet thuộc một AZ riêng có 1 NAT gateway riêng.
                - Tạo route đi ra internet cho các private subnet
                    
                    ```bash
                    resource "aws_route" "route_private_subnet" {
                      count                  = length(aws_route_table.route_table_private_subnet)
                      route_table_id         = aws_route_table.route_table_private_subnet[count.index].id
                      destination_cidr_block = "0.0.0.0/0"
                      nat_gateway_id         = aws_nat_gateway.nat_gw[count.index].id
                    }
                    ```
                    
                    - Mỗi private subnet sẽ có 1 NAT gateway riêng
                - Associate các private subnet vào route table
                    
                    ```bash
                    resource "aws_route_table_association" "route_table_private_subnet_association" {
                      count          = length(aws_route_table.route_table_private_subnet)
                      route_table_id = aws_route_table.route_table_private_subnet[count.index].id
                      subnet_id      = aws_subnet.private_subnet[count.index].id
                    }
                    ```
                    
                    - Associate subnet vào route table tương ứng.
            - Output (vpc.tf)
                
                ```bash
                output "vpc" {
                  value = aws_vpc.eks_vpc
                }
                
                output "public_subnets" {
                  value = aws_subnet.public_subnet
                }
                
                output "private_subnets" {
                  value = aws_subnet.private_subnet
                }
                ```
                
                - Trả về vpc, public subnets, private subnets.
        2. Tạo EKS cluster
            - Tạo variables (variables.tf)
                
                ```bash
                variable "region" {
                  default = "ap-southeast-1"
                }
                variable "cidr_block" {
                  default = "10.0.0.0/16"
                }
                variable "cidr_block_public_subnet" {
                  default = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
                }
                variable "cidr_block_private_subnet" {
                  default = ["10.0.4.0/24", "10.0.5.0/24", "10.0.6.0/24"]
                }
                variable "cluster_name" {
                  default = "microservice"
                }
                ```
                
            - Tạo `vpc` từ module vpc (main.tf)
                
                ```bash
                module "vpc" {
                  source = "./vpc"
                
                  region                   = var.region
                  cidr_block               = var.cidr_block
                  cidr_block_public_subnet = var.cidr_block_public_subnet
                }
                ```
                
                - `source = "./vpc"`: Đường dẫn tới module
                - `region = var.region`: Truyền các biến vào module
            - Tạo `IAM role` cho `EKS Cluster` (iam.tf)
                - [https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/eks_cluster#example-iam-role-for-eks-cluster](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/eks_cluster#example-iam-role-for-eks-cluster)
                
                ```bash
                # IAM Role for cluster
                resource "aws_iam_role" "iam_role" {
                  name = "eks-cluster-iam-role"
                
                  assume_role_policy = <<POLICY
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {
                        "Service": "eks.amazonaws.com"
                      },
                      "Action": "sts:AssumeRole"
                    }
                  ]
                }
                POLICY
                }
                ```
                
                - gán `policy` vào `iam role`
                    
                    ```bash
                    resource "aws_iam_role_policy_attachment" "iam-role-AmazonEKSClusterPolicy" {
                      policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
                      role       = aws_iam_role.iam_role.name
                    }
                    
                    resource "aws_iam_role_policy_attachment" "iam-role-AmazonEKSVPCResourceController" {
                      policy_arn = "arn:aws:iam::aws:policy/AmazonEKSVPCResourceController"
                      role       = aws_iam_role.iam_role.name
                    }
                    ```
                    
                    - `AmazonEKSClusterPolicy`: Các quyền cung cấp cho k8s các quyền để nó có thể quản lý tài nguyên, như CreateTags EC2, Security Group, Elastic Network interface, load balancer, auto scaling,...
                    - `AmazonEKSVPCResourceController`: Các quyền quản lý Elastic Network Interface (ENI) và các IP cho các worker nodes.
            - Tạo `IAM role` cho `Node group` (iam.tf)
                - [https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/eks_node_group#example-iam-role-for-eks-node-group](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/eks_node_group#example-iam-role-for-eks-node-group)
                - Các `IAM role` này sẽ được gán cho các `worker node` được quản lý bởi `node group`
                
                ```bash
                # IAM Role for node group
                resource "aws_iam_role" "iam_role_node_group" {
                  name = "eks-node-group"
                
                  assume_role_policy = jsonencode({
                    Statement = [{
                      Action = "sts:AssumeRole"
                      Effect = "Allow"
                      Principal = {
                        Service = "ec2.amazonaws.com"
                      }
                    }]
                    Version = "2012-10-17"
                  })
                }
                ```
                
                - Gán các `policy` vào `iam role`
                    
                    ```bash
                    resource "aws_iam_role_policy_attachment" "iam_role_node_group-AmazonEKSWorkerNodePolicy" {
                      policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
                      role       = aws_iam_role.iam_role_node_group.name
                    }
                    
                    resource "aws_iam_role_policy_attachment" "iam_role_node_group-AmazonEKS_CNI_Policy" {
                      policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
                      role       = aws_iam_role.iam_role_node_group.name
                    }
                    ```
                    
                    - `AmazonEKSWorkerNodePolicy`: Cho phép EKS worker nodes kết nối tới EKS Cluster.
                    - `AmazonEKS_CNI_Policy`: Các quyền sửa đội cấu hình địa chỉ IP trên các worker node, cho phép liệt kê, sửa đổi các network interface.
            - Tạo keypair cho các worker node
                - [https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/key_pair](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/key_pair)
                - Tạo key pair
                    
                    ```bash
                    $ ssh-keygen
                    ```
                    
                
                ```bash
                resource "aws_key_pair" "node_group_keypair" {
                  key_name   = "node_group_keypair"
                  public_key = file("node_group_keypair.pub")
                }
                ```
                
                - `public_key = file("node_group_keypair.pub")`: Đường dẫn tới public key
            - Tạo `EKS cluster`
                - [https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/eks_cluster](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/eks_cluster)
                
                ```bash
                resource "aws_eks_cluster" "cluster" {
                  name     = "microservice"
                  role_arn = aws_iam_role.iam_role.arn
                
                  vpc_config {
                    subnet_ids = [for subnet in module.vpc.public_subnets : subnet.id]
                  }
                
                  depends_on = [
                    aws_iam_role_policy_attachment.iam-role-AmazonEKSClusterPolicy,
                    aws_iam_role_policy_attachment.iam-role-AmazonEKSVPCResourceController,
                  ]
                }
                ```
                
                - `role_arn = aws_iam_role.iam_role.arn`: `iam role` cho cluster
                - `vpc_config`: VPC được liên kết với cluster
                - `subnet_ids = [for subnet in module.vpc.public_subnets : subnet.id]`: Danh sách subnet, EKS sẽ tạo các elastic network interface ở các subnet này để cho phép giao tiếp giữa worker node và control plane.
                - `depends_on`: Đảm bảo các quyền đã được gán trước khi tạo `EKS cluster`
            - Tạo `node group`
                - [https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/eks_node_group](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/eks_node_group)
                
                ```bash
                resource "aws_eks_node_group" "node_group" {
                  cluster_name    = aws_eks_cluster.cluster.name
                  node_group_name = "node-group"
                  node_role_arn   = aws_iam_role.iam_role_node_group.arn
                  subnet_ids      = [for subnet in module.vpc.private_subnets : subnet.id]
                
                  instance_types = ["t2.micro"]
                
                  remote_access {
                    ec2_ssh_key = aws_key_pair.node_group_keypair.key_name
                  }
                
                  scaling_config {
                    desired_size = 3
                    max_size     = 3
                    min_size     = 1
                  }
                
                  update_config {
                    max_unavailable = 2
                  }
                
                  depends_on = [
                    aws_iam_role_policy_attachment.iam_role_node_group-AmazonEKSWorkerNodePolicy,
                    aws_iam_role_policy_attachment.iam_role_node_group-AmazonEKS_CNI_Policy,
                    aws_iam_role_policy_attachment.iam_role_node_group-AmazonEC2ContainerRegistryReadOnly,
                  ]
                }
                ```
                
                - `cluster_name = aws_eks_cluster.cluster.name`: cluster đã tạo trước đó
                - `node_role_arn   = aws_iam_role.iam_role_node_group.arn`: `iam role` cho các `worker node` được tạo ra bởi `node group`
                - `subnet_ids = [for subnet in module.vpc.private_subnets : subnet.id]`: Nơi các worker node được tạo ra.
                - `ec2_ssh_key`: Trỏ tới keypair đã được tạo.
                - `scaling_config`
                    - `desired_size = 3`: số lượng worker node mong muốn
                    - `max_size = 5`: Số lượng woker node tối đa
                    - `min_size = 3`: Số lượng worker node tối thiểu
                - `max_unavailable`: Số lượng worker node không khả dụng tối đa trong quá trình cập nhật node group.
                - `depends_on`: Đảm bảo các quyền được gán trước khi tạo node group
        - Output
            
            ```bash
            output "endpoint" {
              value = aws_eks_cluster.cluster.endpoint
            }
            ```
            
            - trả về cluster endpoint
        - Apply
            
            ```yaml
            $ terraform apply --auto-approve
            ```
            
        - Kết quả
            
            ![Untitled](https://i.imgur.com/zIqVLDA.png)
          
                
3. Tài liệu tham khảo
    - [https://helpex.vn/article/trien-khai-mot-cum-kubernetes-voi-amazon-eks-6096bce8c5512025d4b405d9](https://helpex.vn/article/trien-khai-mot-cum-kubernetes-voi-amazon-eks-6096bce8c5512025d4b405d9)
    - [https://viblo.asia/p/thuc-hanh-set-up-kubernetes-cluster-tren-amazon-web-services-elastic-kubernetes-service-Qbq5QQEz5D8](https://viblo.asia/p/thuc-hanh-set-up-kubernetes-cluster-tren-amazon-web-services-elastic-kubernetes-service-Qbq5QQEz5D8)
    - [https://kubernetes.github.io/ingress-nginx/deploy/#aws](https://kubernetes.github.io/ingress-nginx/deploy/#aws)
    - [https://stackoverflow.com/questions/64965832/aws-eks-only-2-pod-can-be-launched-too-many-pods-error](https://stackoverflow.com/questions/64965832/aws-eks-only-2-pod-can-be-launched-too-many-pods-error)