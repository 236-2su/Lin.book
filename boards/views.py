from rest_framework import status, viewsets
from rest_framework.decorators import action
from rest_framework.response import Response

from club.models import ClubMember

from .models import Board, BoardLikes, CommentLikes, Comments
from .serializers import BoardSerializer, CommentsSerializer


class BoardViewSet(viewsets.ModelViewSet):
    queryset = Board.objects.select_related("author")
    serializer_class = BoardSerializer

    @action(detail=True, methods=["post"])
    def like(self, request, pk=None):
        board = self.get_object()
        user = ClubMember.objects.first()

        try:
            like = BoardLikes.objects.get(board=board, user=user)
            like.delete()
            return Response(status=status.HTTP_204_NO_CONTENT)
        except BoardLikes.DoesNotExist:
            BoardLikes.objects.create(board=board, user=user)
            return Response(status=status.HTTP_201_CREATED)


class CommentsViewSet(viewsets.ModelViewSet):
    queryset = Comments.objects.select_related("author")
    serializer_class = CommentsSerializer

    @action(detail=True, methods=["post"])
    def like(self, request, pk=None):
        comment = self.get_object()
        user = ClubMember.objects.first()

        try:
            like = CommentLikes.objects.get(comment=comment, user=user)
            like.delete()
            return Response(status=status.HTTP_204_NO_CONTENT)
        except CommentLikes.DoesNotExist:
            CommentLikes.objects.create(comment=comment, user=user)
            return Response(status=status.HTTP_201_CREATED)
