from django.db import models

from club.models import Club, ClubMember


class Board(models.Model):
    BOARD_TYPE_CHOICES = [("announcement", "공지"), ("forum", "자유 게시판")]
    author = models.ForeignKey(ClubMember, on_delete=models.SET_NULL, null=True)
    type = models.CharField(choices=BOARD_TYPE_CHOICES, max_length=50)
    title = models.CharField(max_length=50)
    content = models.TextField()
    views = models.IntegerField(default=0)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    club = models.ForeignKey(Club, on_delete=models.CASCADE)


class AttachedFiles(models.Model):
    board = models.ForeignKey(Board, on_delete=models.CASCADE)
    file = models.FileField(upload_to="attached_files/", max_length=100)


class BoardLikes(models.Model):
    board = models.ForeignKey(Board, on_delete=models.CASCADE)
    user = models.ForeignKey(ClubMember, on_delete=models.CASCADE)

    class Meta:
        unique_together = ("board", "user")


class Comments(models.Model):
    board = models.ForeignKey(Board, on_delete=models.CASCADE)
    author = models.ForeignKey(ClubMember, on_delete=models.CASCADE)
    content = models.TextField()
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)


class CommentLikes(models.Model):
    comment = models.ForeignKey(Comments, on_delete=models.CASCADE)
    user = models.ForeignKey(ClubMember, on_delete=models.CASCADE)

    class Meta:
        unique_together = ("comment", "user")
