from django.urls import include, path
from rest_framework.routers import DefaultRouter

from club.views import UserUnpaidDuesView

from .views import UserViewSet

router = DefaultRouter()
router.register(r"", UserViewSet, basename="user")

urlpatterns = [
    path("", include(router.urls)),
    path("<int:user_id>/unpaid-dues/", UserUnpaidDuesView.as_view(), name="user-unpaid-dues"),
]
