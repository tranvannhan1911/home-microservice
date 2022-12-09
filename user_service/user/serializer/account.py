from rest_framework import serializers

from user.models import User

class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        exclude = ('is_superuser', 'groups', 'is_staff', 'user_permissions')
        read_only_fields = ('is_active', )
        extra_kwargs = {
            'password': {'write_only': True}
        }

class UpdateUserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        exclude = ('is_superuser', 'groups', 'is_staff', 'user_permissions', 'password')
        read_only_fields = ('is_active', 'username')

class PasswordSerializer(serializers.Serializer):
    old_password = serializers.CharField()
    new_password = serializers.CharField()
