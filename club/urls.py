from django.urls import include, path
from rest_framework import routers
from rest_framework_nested import routers as nested_routers

from boards.views import BoardViewSet, CommentsViewSet
from ledger.views import LedgerTransactionsViewSet, LedgerViewSet, ReceiptViewSet

from .views import ClubMemberViewSet, ClubViewSet, ClubWelcomePageViewSet

router = routers.SimpleRouter()
router.register(r"", ClubViewSet, basename="club")

members_router = nested_routers.NestedSimpleRouter(router, r"", lookup="club")
members_router.register(r"members", ClubMemberViewSet, basename="club-members")

boards_router = nested_routers.NestedSimpleRouter(router, r"", lookup="club")
boards_router.register(r"boards", BoardViewSet, basename="club-boards")

comments_router = nested_routers.NestedSimpleRouter(boards_router, r"boards", lookup="board")
comments_router.register(r"comments", CommentsViewSet, basename="board-comments")

ledgers_router = nested_routers.NestedSimpleRouter(router, r"", lookup="club")
ledgers_router.register(r"ledger", LedgerViewSet, basename="club-ledgers")

transactions_router = nested_routers.NestedSimpleRouter(ledgers_router, r"ledger", lookup="ledger")
transactions_router.register(r"transactions", LedgerTransactionsViewSet, basename="ledger-transactions")

receipts_router = nested_routers.NestedSimpleRouter(ledgers_router, r"ledger", lookup="ledger")
receipts_router.register(r"receipts", ReceiptViewSet, basename="ledger-receipts")

welcome_router = nested_routers.NestedSimpleRouter(router, r"", lookup="club")
welcome_router.register(r"welcome", ClubWelcomePageViewSet, basename="club-welcome")


urlpatterns = [
    path("", include(router.urls)),
    path("", include(members_router.urls)),
    path("", include(boards_router.urls)),
    path("", include(comments_router.urls)),
    path("", include(ledgers_router.urls)),
    path("", include(transactions_router.urls)),
    path("", include(receipts_router.urls)),
    path("", include(welcome_router.urls)),
]
