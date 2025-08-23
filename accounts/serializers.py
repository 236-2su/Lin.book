from rest_framework import serializers

from .models import Accounts, AccountTransactions


class AccountSerializer(serializers.ModelSerializer):
    class Meta:
        model = Accounts
        fields = "__all__"
        read_only_fields = ("code", "amount")


class AccountCreateRequestSerializer(serializers.ModelSerializer):
    class Meta:
        model = Accounts
        fields = ["user"]
