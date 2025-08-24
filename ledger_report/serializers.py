from rest_framework import serializers


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
    daily_series = DailyPointSerializer(many=True)
