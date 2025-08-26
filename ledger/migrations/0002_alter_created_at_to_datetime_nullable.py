from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("ledger", "0001_initial"),
    ]

    operations = [
        migrations.AlterField(
            model_name="ledger",
            name="created_at",
            field=models.DateTimeField(null=True, blank=True),
        ),
        migrations.AlterField(
            model_name="receipt",
            name="created_at",
            field=models.DateTimeField(null=True, blank=True),
        ),
    ]
