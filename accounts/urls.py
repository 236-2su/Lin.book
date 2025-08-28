from django.urls import include, path
from rest_framework.routers import DefaultRouter

from .views import AccountLookupViewSet

router = DefaultRouter()
router.register(r"", AccountLookupViewSet, basename="account-lookup")

urlpatterns = [
    path("", include(router.urls)),
]
