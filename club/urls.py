from django.urls import include, path
from rest_framework.routers import DefaultRouter

from .views import ClubMemberViewSet, ClubViewSet

router = DefaultRouter()
router.register(r"clubs", ClubViewSet)
router.register(r"club-members", ClubMemberViewSet)

urlpatterns = [
    path("", include(router.urls)),
]
