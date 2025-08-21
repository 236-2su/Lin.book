from django.urls import include, path
from rest_framework.routers import DefaultRouter

from .views import LedgerTransactionsViewSet, LedgerViewSet, ReceiptViewSet

router = DefaultRouter()
router.register(r"", LedgerViewSet)
router.register(r"transactions", LedgerTransactionsViewSet)
router.register(r"receipts", ReceiptViewSet)

urlpatterns = [path("", include(router.urls))]
