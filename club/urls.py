from django.urls import include, path
from rest_framework import routers
from rest_framework_nested import routers as nested_routers

from accounts.views import ClubAccountsViewSet
from boards.views import BoardViewSet, CommentsViewSet
from ledger.views import (
    EventViewSet,
    LedgerTransactionCommentsViewSet,
    LedgerTransactionsViewSet,
    LedgerViewSet,
    ReceiptViewSet,
)

from .views import ClubMemberViewSet, ClubViewSet, ClubWelcomePageViewSet, SimilarClubsById, SimilarClubsByQuery

router = routers.SimpleRouter()
router.lookup_value_regex = r"\d+"
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

# Register comments under transactions
ledgertransactioncomments_router = nested_routers.NestedSimpleRouter(
    transactions_router, r"transactions", lookup="transaction"
)
ledgertransactioncomments_router.register(
    r"comments", LedgerTransactionCommentsViewSet, basename="transaction-comments"
)


receipts_router = nested_routers.NestedSimpleRouter(ledgers_router, r"ledger", lookup="ledger")
receipts_router.register(r"receipts", ReceiptViewSet, basename="ledger-receipts")

welcome_router = nested_routers.NestedSimpleRouter(router, r"", lookup="club")
welcome_router.register(r"welcome", ClubWelcomePageViewSet, basename="club-welcome")


events_router = nested_routers.NestedSimpleRouter(router, r"", lookup="club")
events_router.register(r"events", EventViewSet, basename="club-events")

accounts_router = nested_routers.NestedSimpleRouter(router, r"", lookup="club")
accounts_router.register(r"accounts", ClubAccountsViewSet, basename="club-accounts")


urlpatterns = [
    path("similar/", SimilarClubsByQuery.as_view(), name="club-similar-by-query"),
    path("<int:club_id>/similar/", SimilarClubsById.as_view(), name="club-similar-by-id"),
    path("", include(router.urls)),
    path("", include(members_router.urls)),
    path("", include(boards_router.urls)),
    path("", include(comments_router.urls)),
    path("", include(ledgers_router.urls)),
    path("", include(transactions_router.urls)),
    path("", include(ledgertransactioncomments_router.urls)),
    path("", include(receipts_router.urls)),
    path("", include(welcome_router.urls)),
    path("", include(events_router.urls)),
    path("", include(accounts_router.urls)),
]
