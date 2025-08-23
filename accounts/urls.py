from django.urls import include, path
from rest_framework.routers import DefaultRouter
from rest_framework_nested import routers as nested_routers

from user.views import UserViewSet

from .views import AccountsViewSet

router = DefaultRouter()
router.register(r"users", UserViewSet, basename="user")

accounts_router = nested_routers.NestedSimpleRouter(router, r"users", lookup="user")
accounts_router.register(r"accounts", AccountsViewSet, basename="user-accounts")

urlpatterns = [
    path("", include(router.urls)),
    path("", include(accounts_router.urls)),
]
