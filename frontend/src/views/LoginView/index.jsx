export default function LoginView() {
  return (
    <section className="generic-page">
      <span className="material-symbols-outlined generic-page-icon" aria-hidden="true">
        login
      </span>
      <h2>Ingresar al SGM</h2>
      <p>
        El formulario de ingreso llega con TPS-14. Hasta entonces, el servidor de desarrollo entra con una sesión de
        desarrollo que ve todos los módulos (<code>VITE_DEV_SESSION</code> en <code>.env.development</code>).
      </p>
    </section>
  );
}
