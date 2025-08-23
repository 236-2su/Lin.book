from django.urls import include, path
from rest_framework.routers import DefaultRouter
from rest_framework_nested import routers

from accounts.views import AccountsViewSet

from .views import UserViewSet

router = DefaultRouter()
router.register(r"", UserViewSet, basename="user")

accounts_router = routers.NestedSimpleRouter(router, r"", lookup="user")
accounts_router.register(r"accounts", AccountsViewSet, basename="user-accounts")

urlpatterns = [
    path("", include(router.urls)),
    path("", include(accounts_router.urls)),
]
