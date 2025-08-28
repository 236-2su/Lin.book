from django.urls import path

from . import views

urlpatterns = [
    path("ping/", views.ping),
    path("rest/", views.test_rest),
    path("ocr/", views.test_ocr),
]
