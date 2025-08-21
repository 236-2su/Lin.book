from django.db import models


class User(models.Model):
    STATUS_CHOICES = [
        ("active", "재학중"),
        ("absence", "휴학중"),
        ("expelled", "제적"),
        ("graduated", "졸업"),
    ]

    name = models.CharField(max_length=50)
    student_number = models.CharField(max_length=50, unique=True)
    admission_year = models.IntegerField()
    phone_number = models.TextField()
    status = models.CharField(max_length=50, choices=STATUS_CHOICES)
    profile_url_image = models.TextField()
