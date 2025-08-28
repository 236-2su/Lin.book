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
        exclude = ["club"]


class EventTransactionSerializer(serializers.ModelSerializer):
    transaction_amount = serializers.SerializerMethodField()
    transaction_type = serializers.SerializerMethodField()
    transaction_date_time = serializers.SerializerMethodField()

    class Meta:
        model = Event
        fields = ["transaction_amount", "transaction_type", "transaction_datetime"]

    def get_transaction_amount(self, obj):
        return obj.ledgertransactions.amount

    def get_transaction_type(self, obj):
        return obj.ledgertransactions.type

    def get_transaction_date_time(self, obj):
        return obj.ledgertransactions.date_time
