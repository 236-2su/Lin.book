from django.urls import include, path
from rest_framework.routers import DefaultRouter

from .views import BoardViewSet, CommentsViewSet

router = DefaultRouter()
router.register(r"", BoardViewSet)
router.register(r"comments", CommentsViewSet)

urlpatterns = [
    path("", include(router.urls)),
]
