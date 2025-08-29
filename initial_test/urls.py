from django.urls import path

from . import views

urlpatterns = [
    path("ping/", views.ping),
    path("ocr/", views.test_ocr),
]
