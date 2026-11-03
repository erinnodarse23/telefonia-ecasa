import { FiInbox } from 'react-icons/fi';

const EmptyState = ({ message = 'No hay datos disponibles', icon: Icon = FiInbox, children }) => (
  <tr>
    <td colSpan={99} className="empty-state">
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
        <Icon size={40} style={{ opacity: 0.3 }} />
        <p>{message}</p>
        {children}
      </div>
    </td>
  </tr>
);

export default EmptyState;
