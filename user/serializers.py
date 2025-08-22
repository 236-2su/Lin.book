from django.contrib.auth import get_user_model
from rest_framework import serializers
from rest_framework.validators import UniqueValidator

User = get_user_model()


class UserSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True, required=False, allow_blank=False)

    email = serializers.EmailField(
        validators=[UniqueValidator(queryset=User.objects.all(), message="이미 등록된 이메일입니다.")]
    )
    student_number = serializers.CharField(
        validators=[UniqueValidator(queryset=User.objects.all(), message="이미 등록된 학번입니다.")]
    )

    class Meta:
        model = User
        fields = [
            "id",
            "username",
            "email",
            "student_number",
            "admission_year",
            "phone_number",
            "status",
            "profile_url_image",
            "user_key",
            "password",
        ]
        read_only_fields = [
            "id",
            "user_key",
        ]

    def create(self, validated_data):
        password = validated_data.pop("password", None)
        user = User.objects.create_user(password=password, **validated_data)
        return user

    def update(self, instance, validated_data):
        password = validated_data.pop("password", None)
        for attr, value in validated_data.items():
            setattr(instance, attr, value)
        if password:
            instance.set_password(password)
        instance.save()
        return instance


class LoginRequestSerializer(serializers.Serializer):
    email = serializers.EmailField()


class LoginResponseSerializer(serializers.Serializer):
    pk = serializers.IntegerField()
