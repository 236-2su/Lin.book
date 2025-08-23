from rest_framework import serializers

from .models import Club, ClubMember


class ClubSerializer(serializers.ModelSerializer):
    class Meta:
        model = Club
        fields = "__all__"


class ClubMemberSerializer(serializers.ModelSerializer):
    class Meta:
        model = ClubMember
        fields = "__all__"


class ClubLoginRequestSerializer(serializers.Serializer):
    email = serializers.EmailField()


class ClubLoginResponseSerializer(serializers.Serializer):
    pk = serializers.IntegerField()
