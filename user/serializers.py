from rest_framework import serializers
from rest_framework.validators import UniqueValidator

from .models import User


class UserSerializer(serializers.ModelSerializer):
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
            "name",
            "email",
            "student_number",
            "major",
            "admission_year",
            "phone_number",
            "status",
            "profile_url_image",
            "user_key",
        ]
        read_only_fields = [
            "id",
            "user_key",
        ]

    def update(self, instance, validated_data):
        for attr, value in validated_data.items():
            setattr(instance, attr, value)
        instance.save()
        return instance


class UserCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = [
            "name",
            "email",
            "student_number",
            "major",
            "admission_year",
            "phone_number",
            "status",
        ]


class LoginRequestSerializer(serializers.Serializer):
    email = serializers.EmailField()


class LoginResponseSerializer(serializers.Serializer):
    pk = serializers.IntegerField()
    club_pks = serializers.ListField(child=serializers.IntegerField(), required=False)


class UserUpdateSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = [
            "major",
            "admission_year",
            "phone_number",
            "status",
            "profile_url_image",
        ]
