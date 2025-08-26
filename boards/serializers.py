from rest_framework import serializers

from user.models import User

from .models import AttachedFiles, Board, BoardLikes, CommentLikes, Comments


class BoardSerializer(serializers.ModelSerializer):
    likes = serializers.SerializerMethodField()
    comments = serializers.SerializerMethodField()

    class Meta:
        model = Board
        fields = "__all__"

    def get_likes(self, obj):
        return obj.boardlikes_set.count()

    def get_comments(self, obj):
        return obj.comments_set.count()


class BoardCreateSerializer(serializers.ModelSerializer):
    # 생성 시에는 사용자 ID를 받아 뷰에서 ClubMember로 매핑
    author = serializers.IntegerField(write_only=True)
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

    class Meta:
        model = Comments
        fields = "__all__"

    def get_likes(self, obj):
        return obj.commentlikes_set.count()


class CommentLikesSerializer(serializers.ModelSerializer):
    class Meta:
        model = CommentLikes
        fields = "__all__"
