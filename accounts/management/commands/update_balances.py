from django.core.management.base import BaseCommand
from django.db import transaction

from accounts.models import Accounts
from accounts.services import get_account_balance


class Command(BaseCommand):
    help = "Updates the balance of all registered accounts from the external API"

    def handle(self, *args, **options):
        self.stdout.write(self.style.SUCCESS("Starting account balance update..."))

        accounts = Accounts.objects.select_related("user").all()
        updated_count = 0
        failed_count = 0

        for account in accounts:
            try:
                self.stdout.write(f"Updating balance for account: {account.code} for user {account.user.email}")

                # Call the service to get the balance
                balance_data = get_account_balance(user=account.user, account_no=account.code)

                # The API guide does not specify the response field for balance.
                # We assume it is 'accountBalance'. This might need to be adjusted.
                new_balance = balance_data.get("accountBalance")

                if new_balance is not None:
                    with transaction.atomic():
                        account.amount = int(new_balance)
                        account.save()
                    self.stdout.write(
                        self.style.SUCCESS(
                            f"Successfully updated balance for account {account.code} to {account.amount}"
                        )
                    )
                    updated_count += 1
                else:
                    self.stdout.write(
                        self.style.WARNING(
                            f"Could not find balance information for account {account.code} in API response: {balance_data}"
                        )
                    )
                    failed_count += 1

            except Exception as e:
                self.stderr.write(self.style.ERROR(f"Failed to update balance for account {account.code}: {e}"))
                failed_count += 1

        self.stdout.write(
            self.style.SUCCESS(f"Finished updating balances. {updated_count} updated, {failed_count} failed.")
        )
