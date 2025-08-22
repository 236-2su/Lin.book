from django.urls import include, path
from rest_framework_nested import routers
from rest_framework.routers import DefaultRouter

from .views import BoardViewSet, CommentsViewSet

router = DefaultRouter()
router.register(r"", BoardViewSet)

comments_router = routers.NestedDefaultRouter(router, r"", lookup="board")
comments_router.register(r"comments", CommentsViewSet, basename="board-comments")

urlpatterns = [
    path("", include(router.urls)),
    path("", include(comments_router.urls)),
]
