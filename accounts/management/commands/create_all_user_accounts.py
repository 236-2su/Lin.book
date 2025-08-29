from django.core.management.base import BaseCommand
from django.db import transaction

from accounts.models import Accounts
from accounts.services import create_account, get_account_balance
from user.models import User


class Command(BaseCommand):
    help = "Create a financial account for all users who do not have one."

    @transaction.atomic
    def handle(self, *args, **options):
        users_without_accounts = User.objects.filter(accounts__isnull=True)

        if not users_without_accounts.exists():
            self.stdout.write(self.style.SUCCESS("All users already have an account."))
            return

        self.stdout.write(
            f"Found {users_without_accounts.count()} users without an account. Starting account creation..."
        )

        for user in users_without_accounts:
            self.stdout.write(f"Creating account for user: {user.email} (ID: {user.pk})...")
            try:
                # 1. Create the account via API
                account_no = create_account(user)

                # 2. Get the initial balance
                balance_info = get_account_balance(user, account_no)
                initial_balance = balance_info.get("accountBalance", 0)

                # 3. Save the new account to the database
                Accounts.objects.create(user=user, code=account_no, amount=initial_balance)
                self.stdout.write(
                    self.style.SUCCESS(
                        f"Successfully created account {account_no} for {user.email} with balance {initial_balance}."
                    )
                )

            except Exception as e:
                self.stderr.write(self.style.ERROR(f"Failed to create account for {user.email}: {e}"))

        self.stdout.write(self.style.SUCCESS("Finished creating accounts for all users."))
