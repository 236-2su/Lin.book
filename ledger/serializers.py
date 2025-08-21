from rest_framework import serializers

from .models import Ledger, LedgerTransactions, Receipt


class LedgerSerializer(serializers.ModelSerializer):
    class Meta:
        model = Ledger
        fields = "__all__"


class ReceiptSerializer(serializers.ModelSerializer):
    class Meta:
        model = Receipt
        fields = "__all__"


class LedgerTransactionsSerializer(serializers.ModelSerializer):
    class Meta:
        model = LedgerTransactions
        fields = "__all__"
