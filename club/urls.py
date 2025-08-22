from django.urls import include, path
from rest_framework_nested import routers
from .views import ClubViewSet, ClubMemberViewSet

router = routers.SimpleRouter()
router.register(r"", ClubViewSet)

members_router = routers.NestedSimpleRouter(router, r"", lookup="club")
members_router.register(r"members", ClubMemberViewSet, basename="club-members")


urlpatterns = [
    path("", include(router.urls)),
    path("", include(members_router.urls)),
]
