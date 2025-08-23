from rest_framework import serializers

from .models import Club, User


class ClubSerializer(serializers.ModelSerializer):
    class Meta:
        model = Club
        fields = "__all__"


class ClubMemberSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = "__all__"
