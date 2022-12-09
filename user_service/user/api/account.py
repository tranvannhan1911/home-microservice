from rest_framework import generics
from rest_framework.response import Response
from rest_framework import status
from django.contrib.auth.hashers import make_password
from django.core.mail import send_mail
from rest_framework_simplejwt.authentication import JWTAuthentication
from rest_framework.permissions import IsAuthenticated
from django.contrib.auth import authenticate

from user.models import User
from user.serializer.account import PasswordSerializer, UpdateUserSerializer, UserSerializer

class Register(generics.CreateAPIView):
    serializer_class = UserSerializer
    def post(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        if(not serializer.is_valid()):
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

        user = serializer.save()
        user.password = make_password(user.password)
        user.save()

        return Response({"data": serializer.data}, status=status.HTTP_200_OK)

class ChangePassword(generics.CreateAPIView):
    authentication_classes = [JWTAuthentication]
    permission_classes = [IsAuthenticated]
    serializer_class = PasswordSerializer
    def post(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        if(not serializer.is_valid()):
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

        if not authenticate(username=request.user.username, password=serializer.data["old_password"]):
            return Response({"error": "Old password invalid!"}, status=status.HTTP_401_UNAUTHORIZED)

        new_password = make_password(serializer.data["new_password"])
        request.user.password = new_password
        request.user.save()
        serializer = UserSerializer(request.user)

        return Response({"data": serializer.data}, status=status.HTTP_200_OK)

class GetUserInfo(generics.CreateAPIView):
    authentication_classes = [JWTAuthentication]
    permission_classes = [IsAuthenticated]

    def get(self, request):
        serializer = UserSerializer(request.user)
        return Response({"data": serializer.data}, status=status.HTTP_200_OK)


class GetUserInfoById(generics.CreateAPIView):
    # authentication_classes = [JWTAuthentication]
    # permission_classes = [IsAuthenticated]

    def get(self, request, id):
        if not User.objects.filter(pk = id).exists():
            return Response({"error": "User invalid!"}, status=status.HTTP_404_NOT_FOUND)

        user = User.objects.get(pk = id)
        serializer = UserSerializer(user)
        return Response({"data": serializer.data}, status=status.HTTP_200_OK)

    def put(self, request, id):
        if not User.objects.filter(pk = id).exists():
            return Response({"error": "User invalid!"}, status=status.HTTP_404_NOT_FOUND)

        user = User.objects.get(pk = id)
        serializer = UpdateUserSerializer(user, data=request.data)
        if serializer.is_valid() == False:
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
            
        serializer.save()

        return Response({"data": serializer.data}, status=status.HTTP_200_OK)