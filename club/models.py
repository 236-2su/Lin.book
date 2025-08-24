from django.core.exceptions import ValidationError
from django.db import models

from user.models import User


class Club(models.Model):
    CLUB_MAJOR_CATEGORY_CHOICES = [
        ("academic", "학술"),
        ("sports", "체육"),
        ("culture", "문화예술"),
        ("volunteer", "봉사"),
        ("entrepreneur", "창업"),
        ("religion", "종교"),
    ]

    name = models.CharField(max_length=50)
    department = models.CharField(max_length=20)
    major_category = models.CharField(max_length=20, choices=CLUB_MAJOR_CATEGORY_CHOICES)
    minor_category = models.CharField(max_length=50)
    description = models.TextField()
    hashtags = models.TextField()
    created_at = models.DateField(auto_now_add=True)
    location = models.TextField()
    short_description = models.TextField()


class ClubWelcomePage(models.Model):
    club = models.OneToOneField(Club, on_delete=models.CASCADE)
    image = models.ImageField(upload_to="welcome/")
    content = models.TextField()


class ClubMember(models.Model):
    CLUB_MEMBER_STATUS_CHOICES = [
        ("active", "활동중"),
        ("absence", "휴학중"),
        ("expelled", "탈퇴"),
        ("graduated", "졸업"),
        ("waiting", "가입 대기 중"),
    ]

    CLUB_MEMBER_ROLE_CHOICES = [
        ("leader", "회장"),
        ("officer", "간부"),
        ("member", "부원"),
    ]

    club = models.ForeignKey(Club, on_delete=models.CASCADE)
    user = models.ForeignKey(User, on_delete=models.SET_NULL, null=True)
    status = models.CharField(max_length=20, choices=CLUB_MEMBER_STATUS_CHOICES, default="active")
    role = models.CharField(max_length=20, choices=CLUB_MEMBER_ROLE_CHOICES, default="member")
    joined_at = models.DateField(auto_now_add=True)
    amount_fee = models.IntegerField()  # 내야 하는 회비 총액
    paid_fee = models.IntegerField()  # 낸 회비 총액


class Dues(models.Model):
    member = models.ForeignKey(ClubMember, on_delete=models.CASCADE)
    amount = models.IntegerField()
    due_to = models.DateField()
    paid_at = models.DateField(null=True, blank=True)
