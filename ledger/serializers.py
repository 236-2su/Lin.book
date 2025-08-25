from rest_framework import serializers

from .models import Event, Ledger, LedgerTransactions, Receipt


class LedgerSerializer(serializers.ModelSerializer):
    class Meta:
        model = Ledger
        fields = "__all__"


class LedgerCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = Ledger
        fields = ["account", "name", "admin"]


class ReceiptSerializer(serializers.ModelSerializer):
    class Meta:
        model = Receipt
        fields = "__all__"


class LedgerTransactionsSerializer(serializers.ModelSerializer):
    class Meta:
        model = LedgerTransactions
        fields = "__all__"


class LedgerTransactionCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = LedgerTransactions
        fields = ["date_time", "amount", "type", "payment_method", "description", "receipt", "vendor", "event"]


class EventSerializer(serializers.ModelSerializer):
    class Meta:
        model = Event
        fields = "__all__"