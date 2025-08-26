from rest_framework import serializers

from .models import LedgerReports


class LedgerReportsSerializer(serializers.ModelSerializer):
    class Meta:
        model = LedgerReports
        fields = ("id", "ledger", "title", "content")
        read_only_fields = fields


class SummarySerializer(serializers.Serializer):
    income = serializers.IntegerField()
    expense = serializers.IntegerField()
    net = serializers.IntegerField()


class TypeRowSerializer(serializers.Serializer):
    type = serializers.CharField()
    income = serializers.IntegerField()
    expense = serializers.IntegerField()
    net = serializers.IntegerField()


class PaymentMethodRowSerializer(serializers.Serializer):
    payment_method = serializers.CharField()
    income = serializers.IntegerField()
    expense = serializers.IntegerField()
    net = serializers.IntegerField()


class EventRowSerializer(serializers.Serializer):
    event_name = serializers.CharField()
    income = serializers.IntegerField()
    expense = serializers.IntegerField()
    net = serializers.IntegerField()


class DailyPointSerializer(serializers.Serializer):
    date = serializers.DateField()
    total = serializers.IntegerField()


class MonthlyLedgerStatsResponseSerializer(serializers.Serializer):
    ledger_id = serializers.IntegerField()
    club_id = serializers.IntegerField()
    year = serializers.IntegerField()
    month = serializers.IntegerField()
    period = serializers.DictField(child=serializers.CharField())
    summary = SummarySerializer()
    by_type = TypeRowSerializer(many=True)
    by_payment_method = PaymentMethodRowSerializer(many=True)
    by_event = EventRowSerializer(many=True)
    daily_series = DailyPointSerializer(many=True)


class YearlyLedgerStatsResponseSerializer(serializers.Serializer):
    ledger_id = serializers.IntegerField()
    club_id = serializers.IntegerField()
    year = serializers.IntegerField()
    summary = SummarySerializer()
    by_month = serializers.DictField(child=MonthlyLedgerStatsResponseSerializer())
