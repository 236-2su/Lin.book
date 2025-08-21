from rest_framework import viewsets

from .models import Ledger, LedgerTransactions, Receipt
from .serializers import LedgerSerializer, LedgerTransactionsSerializer, ReceiptSerializer


class LedgerViewSet(viewsets.ModelViewSet):
    queryset = Ledger.objects.all()
    serializer_class = LedgerSerializer


class LedgerTransactionsViewSet(viewsets.ModelViewSet):
    queryset = LedgerTransactions.objects.all()
    serializer_class = LedgerTransactionsSerializer


class ReceiptViewSet(viewsets.ModelViewSet):
    queryset = Receipt.objects.all()
    serializer_class = ReceiptSerializer
