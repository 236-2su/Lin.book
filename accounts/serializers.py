from rest_framework import serializers

from .models import Accounts, AccountTransactions

# --- For Swagger Documentation ---


class AccountResponseSerializer(serializers.ModelSerializer):
    """Serializer for displaying account details in responses."""

    user_name = serializers.SerializerMethodField()

    class Meta:
        model = Accounts
        fields = ["id", "user", "amount", "code", "created_at", "user_name"]
        read_only_fields = fields

    def get_user_name(self, obj):
        return obj.user.name


class AccountCreateRequestSerializer(serializers.Serializer):
    class Meta:
        model = Accounts
        fields = ["user"]


class AccountUpdateRequestSerializer(serializers.Serializer):
    """Account fields are not meant to be updated directly."""

    pass


class AccountInternalSerializer(serializers.ModelSerializer):
    """
    Internal serializer for the AccountsViewSet to handle object creation.
    Not exposed in Swagger documentation.
    """

    user_name = serializers.SerializerMethodField()

    class Meta:
        model = Accounts
        # save() method needs to be able to set user, code, amount
        fields = ["id", "user", "amount", "code", "created_at", "user_name"]
        read_only_fields = ["id", "user", "created_at", "user_name"]
        extra_kwargs = {"amount": {"required": False}, "code": {"required": False}}

    def get_user_name(self, obj):
        return obj.user.name


# --- Other Serializers ---


class AccountTransactionsSerializer(serializers.ModelSerializer):
    class Meta:
        model = AccountTransactions
        fields = "__all__"
