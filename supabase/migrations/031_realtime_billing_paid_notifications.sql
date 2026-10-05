-- Realtime LUNAS notification path for Bmax SuperApps.
CREATE OR REPLACE FUNCTION public.create_billing_paid_notification()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NEW.status = 'PAID'
     AND (TG_OP = 'INSERT' OR COALESCE(OLD.status, '') <> 'PAID')
     AND NEW.biller_id IS NOT NULL THEN
    INSERT INTO public.user_notifications (
      user_id, type, title, message, entity_type, entity_id
    )
    SELECT
      p.id, 'BILLING_PAID', 'Tagihan Lunas',
      'Tagihan periode ' || NEW.period || ' untuk IDPEL ' || c.id_pelanggan ||
      ' (' || COALESCE(c.nama, '-') || ') telah LUNAS.',
      'BILLING', NEW.id
    FROM public.profiles p
    JOIN public.customers c ON c.id = NEW.customer_id
    WHERE p.active = true
      AND p.role = 'BILLER'
      AND p.biller_id = NEW.biller_id
      AND NOT EXISTS (
        SELECT 1 FROM public.user_notifications un
        WHERE un.user_id = p.id
          AND un.type = 'BILLING_PAID'
          AND un.entity_type = 'BILLING'
          AND un.entity_id = NEW.id
      );
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_billings_paid_notification ON public.billings;

CREATE TRIGGER trg_billings_paid_notification
AFTER INSERT OR UPDATE OF status ON public.billings
FOR EACH ROW
EXECUTE FUNCTION public.create_billing_paid_notification();

CREATE INDEX IF NOT EXISTS idx_user_notifications_user_created
ON public.user_notifications (user_id, created_at DESC);
