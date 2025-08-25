from django.urls import path
from django.urls import include, path
from rest_framework.routers import DefaultRouter
from rest_framework_nested import routers

from .views import BoardViewSet, CommentsViewSet

router = DefaultRouter()
router.register(r"", BoardViewSet)

comments_router = routers.NestedDefaultRouter(router, r"", lookup="board")
comments_router.register(r"comments", CommentsViewSet, basename="board-comments")

urlpatterns = [
    # All board and comment URLs are now nested under /clubs/ and handled in club/urls.py
]
