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
    class Meta:
        model = BoardLikes
        fields = "__all__"


class CommentsSerializer(serializers.ModelSerializer):
    class Meta:
        model = Comments
        fields = "__all__"


class CommentLikesSerializer(serializers.ModelSerializer):
    class Meta:
        model = CommentLikes
        fields = "__all__"
