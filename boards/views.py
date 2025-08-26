from django.shortcuts import get_object_or_404
from drf_spectacular.utils import OpenApiExample, OpenApiResponse, extend_schema, extend_schema_view
from drf_spectacular.utils import OpenApiExample, OpenApiResponse, extend_schema, extend_schema_view
from rest_framework import status, viewsets
from rest_framework.decorators import action
from rest_framework.response import Response

from club.models import Club, ClubMember

from .models import Board, BoardLikes, CommentLikes, Comments
from .serializers import (
    BoardCreateSerializer,
    BoardLikesSerializer,
    BoardSerializer,
    CommentLikesSerializer,
    CommentsSerializer,
    LikeCreateSerializer,
)


@extend_schema_view(
    list=extend_schema(
        summary="게시글 전체 목록 조회",
        description="게시글 목록을 조회합니다.",
        responses={200: OpenApiResponse(response=BoardSerializer, description="OK")},
        tags=["Board"],
    ),
    retrieve=extend_schema(
        summary="특정 게시글",
        description="ID로 특정 게시글의 상세 정보를 조회합니다.",
        responses={
            200: OpenApiResponse(BoardSerializer, description="OK"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["Board"],
    ),
    create=extend_schema(
        summary="글 등록",
        description="새 글을 생성합니다.",
        request=BoardCreateSerializer,
        responses={
            201: OpenApiResponse(BoardSerializer, description="Created"),
            400: OpenApiResponse(description="Bad Request"),
        },
        tags=["Board"],
    ),
    update=extend_schema(
        summary="전체 수정 (PUT)",
        description="글의 모든 필드를 갱신합니다.",
        request=BoardSerializer,
        responses={
            200: OpenApiResponse(BoardSerializer, description="OK"),
            400: OpenApiResponse(description="Bad Request"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["Board"],
    ),
    partial_update=extend_schema(
        summary="부분 수정 (PATCH)",
        description="글의 일부 필드만 부분 갱신합니다.",
        request=BoardSerializer,
        responses={
            200: OpenApiResponse(BoardSerializer, description="OK"),
            400: OpenApiResponse(description="Bad Request"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["Board"],
    ),
    destroy=extend_schema(
        summary="삭제",
        description="ID로 특정 글을 삭제합니다.",
        responses={204: OpenApiResponse(description="No Content"), 404: OpenApiResponse(description="Not Found")},
        tags=["Board"],
    ),
)
class BoardViewSet(viewsets.ModelViewSet):
    """
    게시글에 관한 API
    type : announcement(공지사항)
           forum(자유게시판)
    """

    queryset = Board.objects.select_related("author")
    serializer_class = BoardSerializer

    def get_queryset(self):
        return super().get_queryset().filter(club_id=self.kwargs["club_pk"])

    def create(self, request, *args, **kwargs):
        serializer = BoardCreateSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        # 수동 생성: author는 정수(user_pk)로 들어오므로, 해당 클럽의 ClubMember로 매핑
        club = get_object_or_404(Club, pk=self.kwargs["club_pk"])
        user_id = serializer.validated_data["author"]
        author_member = get_object_or_404(ClubMember, club=club, user_id=user_id)

        # Board 인스턴스 직접 생성
        obj = Board.objects.create(
            club=club,
            author=author_member,
            type=serializer.validated_data["type"],
            title=serializer.validated_data["title"],
            content=serializer.validated_data["content"],
        )
        output = BoardSerializer(obj)
        headers = self.get_success_headers(output.data)
        return Response(output.data, status=status.HTTP_201_CREATED, headers=headers)

    def get_serializer_class(self):
        if self.action == "create":
            return BoardCreateSerializer
        return super().get_serializer_class()

    def perform_create(self, serializer):
        # create에서 직접 생성하므로 사용하지 않음
        pass

    @extend_schema(
        summary="게시글 좋아요", description="게시글에 좋아요가 있으면 삭제하고, 없으면 생성합니다.", tags=["Board"]
    )
    @action(detail=True, methods=["post"])
    def like(self, request, pk=None):
        serializer = LikeCreateSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user_id = serializer.validated_data["user_id"]

        board = self.get_object()
        user = get_object_or_404(ClubMember, club=board.club, user_id=user_id)

        try:
            like = BoardLikes.objects.get(board=board, user=user)
            like.delete()
            return Response(status=status.HTTP_204_NO_CONTENT)
        except BoardLikes.DoesNotExist:
            BoardLikes.objects.create(board=board, user=user)
            return Response(status=status.HTTP_201_CREATED)


@extend_schema_view(
    list=extend_schema(
        summary="댓글 전체 목록 조회",
        description="댓글 목록을 조회합니다.",
        responses={200: OpenApiResponse(response=CommentsSerializer, description="OK")},
        tags=["Comments"],
    ),
    retrieve=extend_schema(
        summary="특정 댓글 조회",
        description="ID로 특정 댓글의 상세 정보를 조회합니다.",
        responses={
            200: OpenApiResponse(CommentsSerializer, description="OK"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["Comments"],
    ),
    create=extend_schema(
        summary="댓글 등록",
        description="새 댓글을 생성합니다.",
        request=CommentsSerializer,
        responses={
            201: OpenApiResponse(CommentsSerializer, description="Created"),
            400: OpenApiResponse(description="Bad Request"),
        },
        tags=["Comments"],
    ),
    update=extend_schema(
        summary="댓글 전체 수정 (PUT)",
        description="댓글의 모든 필드를 갱신합니다.",
        request=CommentsSerializer,
        responses={
            200: OpenApiResponse(CommentsSerializer, description="OK"),
            400: OpenApiResponse(description="Bad Request"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["Comments"],
    ),
    partial_update=extend_schema(
        summary="댓글 부분 수정 (PATCH)",
        description="댓글의 일부 필드만 부분 갱신합니다.",
        request=CommentsSerializer,
        responses={
            200: OpenApiResponse(CommentsSerializer, description="OK"),
            400: OpenApiResponse(description="Bad Request"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["Comments"],
    ),
    destroy=extend_schema(
        summary="댓글 삭제",
        description="ID로 특정 댓글을 삭제합니다.",
        responses={204: OpenApiResponse(description="No Content"), 404: OpenApiResponse(description="Not Found")},
        tags=["Comments"],
    ),
)
class CommentsViewSet(viewsets.ModelViewSet):
    queryset = Comments.objects.select_related("author")
    serializer_class = CommentsSerializer

    def get_queryset(self):
        return super().get_queryset().filter(board_id=self.kwargs["board_pk"])

    def perform_create(self, serializer):
        board = get_object_or_404(Board, pk=self.kwargs["board_pk"])
        serializer.save(board=board)

    @extend_schema(
        summary="댓글 좋아요", description="댓글에 좋아요가 있으면 삭제하고, 없으면 생성합니다.", tags=["Comments"]
    )
    @action(detail=True, methods=["post"])
    def like(self, request, pk=None):
        serializer = LikeCreateSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user_id = serializer.validated_data["user_id"]

        comment = self.get_object()
        user = get_object_or_404(ClubMember, club=comment.board.club, user_id=user_id)

        try:
            like = CommentLikes.objects.get(comment=comment, user=user)
            like.delete()
            return Response(status=status.HTTP_204_NO_CONTENT)
        except CommentLikes.DoesNotExist:
            CommentLikes.objects.create(comment=comment, user=user)
            return Response(status=status.HTTP_201_CREATED)
