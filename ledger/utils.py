from django.db.models import Sum

from .models import Ledger, LedgerTransactions


def sync_ledger_amount(ledger: Ledger):
    """
    장부를 거래 내역의 총액으로 동기화
    """
    total = LedgerTransactions.objects.filter(ledger=ledger).aggregate(total=Sum("amount")).get("total") or 0
    ledger.amount = total
    ledger.save(update_fields=["amount"])
    return ledger.amount
