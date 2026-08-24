-- Customer staging import is a server-side operation.
-- Mobile clients must never be able to invoke the SECURITY DEFINER RPC.
REVOKE EXECUTE ON FUNCTION public.import_customer_staging(uuid) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.import_customer_staging(uuid) FROM anon;
REVOKE EXECUTE ON FUNCTION public.import_customer_staging(uuid) FROM authenticated;
GRANT EXECUTE ON FUNCTION public.import_customer_staging(uuid) TO service_role;

COMMENT ON FUNCTION public.import_customer_staging(uuid) IS
  'Server-side/customer-import operation. Execute only with service_role or direct database owner privileges; never expose this RPC to mobile clients.';
