from rest_framework import serializers

from .models import Club, ClubMember


class ClubSerializer(serializers.ModelSerializer):
    class Meta:
        model = Club
        fields = "__all__"


class ClubCreateSerializer(serializers.ModelSerializer):
    admin = serializers.IntegerField(write_only=True)

    class Meta:
        model = Club
        fields = [
            "name",
            "department",
            "major_category",
            "minor_category",
            "description",
            "hashtags",
            "admin",
        ]


class ClubMemberCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = ClubMember
        fields = ["status", "role", "club", "user"]


class ClubMemberSerializer(serializers.ModelSerializer):
    class Meta:
        model = ClubMember
        fields = "__all__"


class ClubLoginRequestSerializer(serializers.Serializer):
    email = serializers.EmailField()
    club_id = serializers.IntegerField()


class ClubLoginResponseSerializer(serializers.Serializer):
    pk = serializers.IntegerField()
