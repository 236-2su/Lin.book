from drf_spectacular.utils import extend_schema_field
from rest_framework import serializers

from user.models import User

from .models import AttachedFiles, Board, BoardLikes, CommentLikes, Comments


class BoardSerializer(serializers.ModelSerializer):
    likes = serializers.SerializerMethodField()
    comments = serializers.SerializerMethodField()
    author_name = serializers.SerializerMethodField()
    author_major = serializers.SerializerMethodField()

    class Meta:
        model = Board
        fields = "__all__"

    @extend_schema_field(serializers.IntegerField())
    def get_likes(self, obj):
        return obj.boardlikes_set.count()

    @extend_schema_field(serializers.IntegerField())
    def get_comments(self, obj):
        return obj.comments_set.count()

    @extend_schema_field(serializers.CharField())
    def get_author_name(self, obj):
        return obj.author.user.name

    @extend_schema_field(serializers.CharField())
    def get_author_major(self, obj):
        return obj.author.user.major


class BoardCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = Board
        fields = ["author", "type", "title", "content"]


class AttachedFilesSerializer(serializers.ModelSerializer):
    class Meta:
        model = AttachedFiles
        fields = "__all__"


class BoardLikesSerializer(serializers.ModelSerializer):
    class Meta:
        model = BoardLikes
        fields = "__all__"


class LikeCreateSerializer(serializers.Serializer):
    user_id = serializers.IntegerField()


class CommentsSerializer(serializers.ModelSerializer):
    likes = serializers.SerializerMethodField()
    author_name = serializers.SerializerMethodField()
    author_major = serializers.SerializerMethodField()

    class Meta:
        model = Comments
        fields = "__all__"

    @extend_schema_field(serializers.IntegerField())
    def get_likes(self, obj):
        return obj.commentlikes_set.count()

    @extend_schema_field(serializers.CharField())
    def get_author_name(self, obj):
        return obj.author.user.name

    @extend_schema_field(serializers.CharField())
    def get_author_major(self, obj):
        return obj.author.user.major


class CommentLikesSerializer(serializers.ModelSerializer):
    class Meta:
        model = CommentLikes
        fields = "__all__"
