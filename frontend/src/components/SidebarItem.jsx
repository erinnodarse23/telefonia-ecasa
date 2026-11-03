import { useState, useRef, startTransition } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { routePrefetch } from '../utils/pagePrefetch';

const SidebarItem = ({ icon, text, to, collapsed }) => {
  const ref = useRef(null);
  const navigate = useNavigate();
  const [tooltip, setTooltip] = useState({ show: false, top: 0, left: 0 });

  const showTooltip = () => {
    if (!collapsed || !ref.current) return;
    const rect = ref.current.getBoundingClientRect();
    const tooltipH = 32;
    let top = rect.top + rect.height / 2;
    if (top - tooltipH / 2 < 8) top = tooltipH / 2 + 8;
    if (top + tooltipH / 2 > window.innerHeight - 8) top = window.innerHeight - tooltipH / 2 - 8;
    let left = rect.right + 12;
    if (left + 200 > window.innerWidth) {
      left = rect.left - 12 - 200;
    }
    setTooltip({ show: true, top, left });
  };

  const hideTooltip = () => setTooltip({ show: false, top: 0, left: 0 });

  const prefetch = () => {
    if (typeof routePrefetch[to] === 'function') routePrefetch[to]();
  };

  const handleClick = (e) => {
    e.preventDefault();
    prefetch();
    startTransition(() => navigate(to));
  };

  return (
    <>
      <NavLink
        ref={ref}
        to={to}
        end
        style={({ isActive }) => ({
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          padding: collapsed ? '0.75rem 0' : '0.625rem 1.25rem',
          justifyContent: collapsed ? 'center' : 'flex-start',
          color: isActive ? '#ffffff' : 'rgba(255,255,255,0.6)',
          textDecoration: 'none',
          fontSize: 14,
          fontWeight: isActive ? 600 : 450,
          transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
          backgroundColor: isActive ? 'rgba(59, 130, 246, 0.2)' : 'transparent',
          borderLeft: isActive && !collapsed ? '3px solid #3b82f6' : '3px solid transparent',
          margin: collapsed ? '0.25rem 0' : '0.25rem 0.5rem',
          borderRadius: collapsed ? '0.5rem' : '0 0.5rem 0.5rem 0',
          position: 'relative',
        })}
        onClick={handleClick}
        onMouseEnter={(e) => {
          prefetch();
          showTooltip();
          if (!e.currentTarget.classList.contains('active'))
            e.currentTarget.style.backgroundColor = 'rgba(255,255,255,0.06)';
        }}
        onMouseLeave={(e) => {
          hideTooltip();
          if (!e.currentTarget.classList.contains('active'))
            e.currentTarget.style.backgroundColor = 'transparent';
        }}
      >
        <span style={{
          fontSize: 18,
          width: 22,
          textAlign: 'center',
          flexShrink: 0,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}>
          {icon}
        </span>
        <span style={{
          opacity: collapsed ? 0 : 1,
          width: collapsed ? 0 : 'auto',
          overflow: 'hidden',
          whiteSpace: 'nowrap',
          transition: 'opacity 0.2s, width 0.2s',
        }}>
          {text}
        </span>
      </NavLink>
      {collapsed && tooltip.show && (
        <span
          className="sidebar-tooltip"
          style={{
            top: tooltip.top,
            left: tooltip.left,
            transform: 'translateY(-50%)',
          }}
        >
          {text}
        </span>
      )}
    </>
  );
};

export default SidebarItem;
