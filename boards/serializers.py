from rest_framework import serializers

from .models import AttachedFiles, Board, BoardLikes, CommentLikes, Comments


class BoardSerializer(serializers.ModelSerializer):
    class Meta:
        model = Board
        fields = "__all__"


class AttachedFilesSerializer(serializers.ModelSerializer):
    class Meta:
        model = AttachedFiles
        fields = "__all__"


class BoardLikesSerializer(serializers.ModelSerializer):
    likes_total = serializers.SerializerMethodField()

    class Meta:
        model = BoardLikes
        fields = "__all__"

    def get_likes_total(self, obj):
        return len(obj.content.split())


class CommentsSerializer(serializers.ModelSerializer):
    class Meta:
        model = Comments
        fields = "__all__"


class CommentLikesSerializer(serializers.ModelSerializer):
    likes_total = serializers.SerializerMethodField()

    class Meta:
        model = CommentLikes
        fields = "__all__"

    def get_likes_total(self, obj):
        return len(obj.content.split())
