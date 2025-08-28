from django.shortcuts import get_object_or_404
from drf_spectacular.utils import OpenApiExample, OpenApiParameter, OpenApiResponse, extend_schema, extend_schema_view
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

    queryset = Board.objects.select_related("author__user")
    serializer_class = BoardSerializer

    @extend_schema(
        summary="게시글 좋아요",
        description="게시글에 좋아요가 있으면 삭제하고, 없으면 생성합니다.",
        tags=["Board"],
        request=LikeCreateSerializer,
        responses={
            201: BoardLikesSerializer,
            204: OpenApiResponse(description="Like removed. No content."),
        },
    )
    @action(detail=True, methods=["post"])
    def like(self, request, pk=None, club_pk=None):
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
            like = BoardLikes.objects.create(board=board, user=user)
            serializer = BoardLikesSerializer(like)
            return Response(serializer.data, status=status.HTTP_201_CREATED)


@extend_schema(
    parameters=[
        OpenApiParameter(name="club_pk", type=int, location=OpenApiParameter.PATH, description="Club ID"),
        OpenApiParameter(name="board_pk", type=int, location=OpenApiParameter.PATH, description="Board ID"),
    ]
)
@extend_schema_view(
    list=extend_schema(
        summary="댓글 전체 목록 조회",
        description="댓글 목록을 조회합니다.",
        responses={200: OpenApiResponse(response=CommentsSerializer, description="OK")},
        tags=["Comments"],
    ),
    retrieve=extend_schema(
        summary="특정 댓글 조회",
        description="comment_pk로 특정 댓글의 상세 정보를 조회합니다.",
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
        description="comment_pk로 특정 댓글을 삭제합니다.",
        responses={204: OpenApiResponse(description="No Content"), 404: OpenApiResponse(description="Not Found")},
        tags=["Comments"],
    ),
)
class CommentsViewSet(viewsets.ModelViewSet):
    queryset = Comments.objects.select_related("author__user")
    serializer_class = CommentsSerializer
    lookup_url_kwarg = "comment_pk"

    def get_queryset(self):
        return super().get_queryset().filter(board_id=self.kwargs["board_pk"])

    def perform_create(self, serializer):
        board = get_object_or_404(Board, pk=self.kwargs["board_pk"])
        serializer.save(board=board)

    @extend_schema(
        summary="댓글 좋아요",
        description="댓글에 좋아요가 있으면 삭제하고, 없으면 생성합니다. request body에 user_id가 필요합니다.",
        tags=["Comments"],
        request=LikeCreateSerializer,
        responses={
            201: OpenApiResponse(description="Like created."),
            204: OpenApiResponse(description="Like removed. No content."),
        },
    )
    @action(detail=True, methods=["post"])
    def like(self, request, club_pk=None, board_pk=None, comment_pk=None):
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
