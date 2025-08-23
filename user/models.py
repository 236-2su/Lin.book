from datetime import date

from django.core.validators import MaxValueValidator, MinValueValidator, RegexValidator
from django.db import models


class User(models.Model):
    class Status(models.TextChoices):
        ACTIVE = "active", "재학중"
        ABSENCE = "absence", "휴학중"
        EXPELLED = "expelled", "제적"
        GRADUATED = "graduated", "졸업"

    email = models.EmailField(unique=True)
    student_number = models.CharField(max_length=50, unique=True, db_index=True)
    admission_year = models.IntegerField(validators=[MinValueValidator(1950), MaxValueValidator(date.today().year)])
    phone_number = models.CharField(
        max_length=20,
        validators=[RegexValidator(r"^\+?\d{7,15}$", message="전화번호 형식이 올바르지 않습니다.")],
        blank=True,
    )
    status = models.CharField(max_length=10, choices=Status.choices, default=Status.ACTIVE)
    profile_url_image = models.URLField(blank=True)
    user_key = models.CharField(max_length=200, blank=True, null=True, db_index=True)
