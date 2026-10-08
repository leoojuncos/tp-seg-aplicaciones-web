import { Message, SectionHeading } from '../../components/common/index.js';

export default function VepView() {
  return (
    <>
      <SectionHeading titles={['Consulta de deuda']} />
      <section className="section-panel">
        <Message>Portal público del VEP. La consulta de deuda por CUIT llega con TPS-21.</Message>
      </section>
    </>
  );
}
