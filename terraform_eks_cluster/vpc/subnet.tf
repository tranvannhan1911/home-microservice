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
