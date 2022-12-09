import random
from django.db import models
from django.contrib.auth.models import AbstractUser, BaseUserManager
# from django.contrib.auth.models import User
from django.dispatch import receiver
from django.db.models.signals import post_save
from django.utils import timezone

class CustomUserManager(BaseUserManager):
    def create_user(self, email, password, **extra_fields):
        if not email:
            raise ValueError('The email must be set')
        email = self.normalize_email(email)
        user = self.model(email=email, **extra_fields)
        user.set_password(password)
        user.save()
        return user

    def create_superuser(self, email, password, **extra_fields):
        extra_fields.setdefault('is_staff', True)
        extra_fields.setdefault('is_superuser', True)
        extra_fields.setdefault('is_active', True)

        if extra_fields.get('is_staff') is not True:
            raise ValueError('Superuser must have is_staff=True.')
        if extra_fields.get('is_superuser') is not True:
            raise ValueError('Superuser must have is_superuser=True.')
        return self.create_user(email, password, **extra_fields)

class User(AbstractUser):
    email = models.EmailField("email address", blank=True)
    phone = models.CharField('phone', max_length=10, null=True)
    date_of_birth = models.DateField(verbose_name='date of birth', default='1900-01-01')
    gender = models.CharField(verbose_name='gender', max_length=1, default='U', choices=(
        ('M', 'Nam'),
        ('F', 'Nữ'),
        ('U', 'Không xác định'),
    ))

    USERNAME_FIELD = 'username'
    REQUIRED_FIELDS = []

    objects = CustomUserManager()

    def __str__(self):
        return self.username