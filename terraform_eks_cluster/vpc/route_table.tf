# route table for public subnets
resource "aws_route_table" "route_table_public_subnet" {
  vpc_id = aws_vpc.eks_vpc.id

  tags = {
    Name = "Route table public subnet"
  }
}

resource "aws_route" "route_public_subnet" {
  route_table_id         = aws_route_table.route_table_public_subnet.id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.igw.id
}

resource "aws_route_table_association" "route_table_public_subnet_association" {
  count          = length(aws_subnet.public_subnet)
  subnet_id      = aws_subnet.public_subnet[count.index].id
  route_table_id = aws_route_table.route_table_public_subnet.id
}
